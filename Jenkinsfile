@Library('dynatrace@master') _
pipeline {
  agent {
    label 'maven'
  }

  environment {
    APP_NAME = "carts"
    VERSION = readFile('version').trim()
    ARTEFACT_ID = "sockshop-" + "${env.APP_NAME}"
    TAG = "${env.DOCKER_REGISTRY_URL}:5000/sockshop-registry/${env.ARTEFACT_ID}"
    TAG_DEV = "${env.TAG}:${env.VERSION}-${env.BUILD_NUMBER}"
    TAG_STAGING = "${env.TAG}:${env.VERSION}"
    DYNATRACEID="${env.DT_ACCOUNTID}"
    DYNATRACEAPIKEY="${env.DT_API_TOKEN}"
    NLAPIKEY="${env.NL_WEB_API_KEY}"
    OUTPUTSANITYCHECK="$WORKSPACE/infrastructure/sanitycheck.json"
    DYNATRACEPLUGINPATH="$WORKSPACE/lib/DynatraceIntegration-3.0.1-SNAPSHOT.jar"
  }
  stages {
    stage('Maven build') {
      steps {
        checkout scm
        container('maven') {
          sh "mvn -B clean package -DdynatraceId=$DYNATRACEID -DneoLoadWebAPIKey=$NLAPIKEY -DdynatraceApiKey=$DYNATRACEAPIKEY -Dtags=${env.APP_NAME} -DoutPutReferenceFile=$OUTPUTSANITYCHECK -DcustomActionPath=$DYNATRACEPLUGINPATH"
        }
      }
    }
    stage('Docker build') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('docker') {
          sh "docker build -t ${env.TAG_DEV} ."
        }
      }
    }
    stage('Docker push to registry') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: 'registry-creds', passwordVariable: 'TOKEN', usernameVariable: 'USER')]) {
            sh "docker login --username=anything --password=${TOKEN} ${env.DOCKER_REGISTRY_URL}:5000"
            sh "docker tag ${env.TAG_DEV} ${env.TAG_DEV}"
            sh "docker push ${env.TAG_DEV}"
          }
        }
      }
    }
    
    stage('Deploy to dev namespace') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('kubectl') {
          sh "sed -i 's#image: .*#image: ${env.TAG_DEV}#' manifest/carts.yml"
          sh "sed -i 's#value: to-be-replaced-by-jenkins.*#value: ${env.VERSION}-${env.BUILD_NUMBER}#' manifest/carts.yml"
          sh "kubectl -n dev apply -f manifest/carts.yml"
        }
      }
    }

  /*  stage('DT Deploy Event') {
      when {
          expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
          }
      }
      steps {
          createDynatraceDeploymentEvent(
          envId: 'Dynatrace Tenant',
          tagMatchRules: [
              [
              meTypes: [
                  [meType: 'SERVICE']
              ],
              tags: [
                  [context: 'CONTEXTLESS', key: 'app', value: "${env.APP_NAME}"],
                  [context: 'CONTEXTLESS', key: 'environment', value: 'dev']
              ]
              ]
          ])
      }
    }*/
    stage('Start NeoLoad infrastructure') {
          /*steps {

                 sh  'kubectl run LG -f $WORKSPACE/infrastructure/infrastructure/neoload/lg/docker-compose.yml'
                 stash includes: '$WORKSPACE/infrastructure/infrastructure/neoload/lg/lg.yaml', name: 'LG'
                 stash includes: '$WORKSPACE/infrastructure/infrastructure/neoload/test/scenario.yaml', name: 'scenario'
            }*/
            steps {
                    container('kubectl') {
                        script {
                         sh "kubectl apply -f $WORKSPACE/infrastructure/infrastructure/neoload/lg/docker-compose.yml"
                    //     sh "cp $WORKSPACE/infrastructure/infrastructure/neoload/license.lic /home/neoload/.neotys/neoload/"
                        }
                    }
                    }

    }
 /*   stage('Deploy NeoLoad License') {
        steps {
                script {
                        sh "cp $WORKSPACE/infrastructure/infrastructure/neoload/license.lic /home/jenkins/.neotys/neoload/"
                }

        }
    }*/
    stage('Run health check in dev')  {
      when {
            expression {
              return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
            }
       }

       steps {

         container('neoload') {
              sh "cp $WORKSPACE/infrastructure/infrastructure/neoload/license.lic /home/neoload/.neotys/neoload/"
             script {
                     def status =neoloadRun executable: '/home/neoload/neoload/bin/NeoLoadCmd',
                                      project: "$WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp",
                                      testName: 'HealthCheck_${BUILD_NUMBER}',
                                      testDescription: 'HealthCheck_${BUILD_NUMBER}',
                                      commandLineOption: "-nlweb -loadGenerators $WORKSPACE/infrastructure/infrastructure/neoload/lg/lg.yaml -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev,port=80,basicPath=/health",
                                      scenario: 'DynatraceSanityCheck',
                                      trendGraphs: [

                                           'AvgResponseTime',
                                           'ErrorRate'
                                      ]
                    if (status != 0) {
                              currentBuild.result = 'FAILED'
                              error "Health check in dev failed."
                    }
              }
          }

      }
    }
    stage('Sanity Check') {
          when {
            expression {
              return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
            }
          }

          steps {
            echo "Waiting for the service to start..."
          //  sleep 150
            container('neoload') {
              script {
                     def status =neoloadRun executable: '/home/neoload/neoload/bin/NeoLoadCmd',
                                      project: "$WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp",
                                      testName: 'SANITYCHECK_${BUILD_NUMBER}',
                                      testDescription: 'SANITYCHECK_${BUILD_NUMBER}',
                                      commandLineOption: "-nlweb -loadGenerators $WORKSPACE/infrastructure/infrastructure/neoload/lg/lg.yaml -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev,port=80,basicPath=/health",
                                      scenario: 'DYNATRACE_SANITYCHECK',
                                      trendGraphs: [
                                           'ErrorRate'
                                      ]
                    if (status != 0) {
                                      currentBuild.result = 'FAILED'
                                      error "Health check in dev failed."
                    }


                }
              }
                sh '''
                       git add OUTPUTSANITYCHECK
                       git commit -am 'Sanity Check ${BUILD_NUMBER}'
                       git push origin master
                  '''

          }
    }
    stage('Run functional check in dev') {
          when {
            expression {
              return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
            }
          }
         /*  agent {
                dockerfile {
                  args '--user root -v /tmp:/tmp --network cpv --env license=$WORKSPACE/infrastructure/infrastructure/neoload/licence.lic'
                  dir '$WORKSPACE/infrastructure/infrastructure/neoload/controller'
                }
            }*/
          steps {
               container('neoload') {
                 script {
                      def status =neoloadRun executable: '/home/neoload/neoload/bin/NeoLoadCmd',
                      project: "$WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp",
                      testName: 'FuncCheck__${BUILD_NUMBER}',
                      testDescription: 'FuncCheck__${BUILD_NUMBER}',
                      commandLineOption: "-nlweb -loadGenerators $WORKSPACE/infrastructure/infrastructure/neoload/lg/lg.yaml -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev,port=80",
                      scenario: 'Cart_Load',
                      trendGraphs: [
                                          [
                                              name: 'Transactions Response Time',
                                              curve: [
                                                          'AddItemToCart>Actions>AddItem'

                                                      ],
                                              statistic: 'average'
                                          ],
                                          [
                                              name: 'User Load',
                                              curve: ['Controller/User Load'],
                                              statistic: 'average'
                                          ],

                                          'AvgResponseTime',
                                          'ErrorRate'
                                  ]

                       if (status != 0) {
                        currentBuild.result = 'FAILED'
                        error "Load Test on cart."
                      }
                 }
             }


          }
    }
    stage('Stop NeoLoad Infrastructure') {
    steps {
              container('kubectl') {
                    script {
                     sh "kubectl delete svc nl-lg -n dev"
                    }
               }
        }
    }
    stage('Mark artifact for staging namespace') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*'
        }
      }
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: 'registry-creds', passwordVariable: 'TOKEN', usernameVariable: 'USER')]) {
            sh "docker login --username=anything --password=${TOKEN} ${env.DOCKER_REGISTRY_URL}:5000"
            sh "docker tag ${env.TAG_DEV} ${env.TAG_STAGING}"
            sh "docker push ${env.TAG_STAGING}"
          }
        }
      }
    }
    stage('Deploy to staging') {
      when {
        beforeAgent true
        expression {
          return env.BRANCH_NAME ==~ 'release/.*'
        }
      }
      steps {
        build job: "k8s-deploy-staging",
          parameters: [
            string(name: 'APP_NAME', value: "${env.APP_NAME}"),
            string(name: 'TAG_STAGING', value: "${env.TAG_STAGING}"),
            string(name: 'VERSION', value: "${env.VERSION}")
          ]
      }
    }
  }
}