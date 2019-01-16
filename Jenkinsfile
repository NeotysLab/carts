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
    NL_DT_TAG="app:${env.APP_NAME},environment:dev"
    CARTS_ANOMALIEFILE="$WORKSPACE/monspec/carts_anomalieDection.json"
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
          sh "mvn -B clean package -DdynatraceId=$DYNATRACEID -DneoLoadWebAPIKey=$NLAPIKEY -DdynatraceApiKey=$DYNATRACEAPIKEY -Dtags=${NL_DT_TAG} -DoutPutReferenceFile=$OUTPUTSANITYCHECK -DcustomActionPath=$DYNATRACEPLUGINPATH -DjsonAnomalieDetectionFile=$CARTS_ANOMALIEFILE"
          sh "chmod -R 777 $WORKSPACE/target/neoload/"
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

    /*stage('DT Deploy Event') {
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

            steps {
                    container('kubectl') {
                        script {
                         sh "kubectl create -f $WORKSPACE/infrastructure/infrastructure/neoload/lg/docker-compose.yml"
                        }
                    }
                    }

    }

    stage('Run health check in dev')  {
      when {
            expression {
              return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
            }
       }

       steps {

         container('neoload') {
             echo "Waiting for the service to start..."
             sleep 300
             script {
                    try {
                       sh "mkdir -p /home/jenkins/.neotys/neoload"
                        sh "cp $WORKSPACE/infrastructure/infrastructure/neoload/license.lic /home/jenkins/.neotys/neoload/"
                           /*
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
                                          */


                         def status =sh "/neoload/bin/NeoLoadCmd -project $WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp -testResultName HealthCheck_${BUILD_NUMBER} -description HealthCheck_${BUILD_NUMBER} -nlweb -L Population_BasicCheckTesting=$WORKSPACE/infrastructure/infrastructure/neoload/lg/remote.txt -L Population_Dynatrace_Integration=$WORKSPACE/infrastructure/infrastructure/neoload/lg/local.txt -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev.svc,port=80,basicPath=/carts/1/items/health -launch DynatraceSanityCheck -noGUI"

                        if (status != 0) {
                                  currentBuild.result = 'FAILED'
                                  error "Health check in dev failed."
                        }
                    }
                    catch (err) {

                    }
              }
          }

      }
    }
    stage('Sanity Check') {
          steps {
            container('neoload') {
              script {
                     def status =sh "/neoload/bin/NeoLoadCmd -project $WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp -testResultName DynatraceSanityCheck_${BUILD_NUMBER} -description DynatraceSanityCheck_${BUILD_NUMBER} -nlweb -L  Population_Dynatrace_SanityCheck=$WORKSPACE/infrastructure/infrastructure/neoload/lg/local.txt -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev,port=80 -launch DYNATRACE_SANITYCHECK  -noGUI"
                      /*
                     def status =neoloadRun executable: '/home/neoload/neoload/bin/NeoLoadCmd',
                                      project: "$WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp",
                                      testName: 'SANITYCHECK_${BUILD_NUMBER}',
                                      testDescription: 'SANITYCHECK_${BUILD_NUMBER}',
                                      commandLineOption: "-nlweb -loadGenerators $WORKSPACE/infrastructure/infrastructure/neoload/lg/lg.yaml -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev,port=80,basicPath=/health",
                                      scenario: 'DYNATRACE_SANITYCHECK',
                                      trendGraphs: [
                                           'ErrorRate'
                                      ]
                      */
                    if (status != 0) {
                                      currentBuild.result = 'FAILED'
                                      error "Health check in dev failed."
                    }


                }
              }
                sh "
                       git add ${OUTPUTSANITYCHECK}
                       git commit -am 'Sanity Check ${BUILD_NUMBER}'
                       git push origin master
                  "

          }
    }
    stage('Run functional check in dev') {
          when {
            expression {
              return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
            }
          }

          steps {
               container('neoload') {
                 script {

                      def status =sh "/neoload/bin/NeoLoadCmd -project $WORKSPACE/target/neoload/Carts_NeoLoad/Carts_NeoLoad.nlp -testResultName FuncCheck__${BUILD_NUMBER} -description FuncCheck__${BUILD_NUMBER} -nlweb -L  Population_AddItemToCart=$WORKSPACE/infrastructure/infrastructure/neoload/lg/remote.txt -L Population_Dynatrace_Integration=$WORKSPACE/infrastructure/infrastructure/neoload/lg/local.txt -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev,port=80 -launch Cart_Load -noGUI"
                       /*
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
                         */

                       if (status != 0) {
                        currentBuild.result = 'FAILED'
                        error "Load Test on cart."
                      }
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
  post {
      always {
        container('kubectl') {
               script {
                echo "delete neoload infrastructure"
                sh "kubectl delete svc nl-lg -n cicd"
                sh "kubectl delete pod nl-lg -n cicd --grace-period=0 --force"
               }
        }
      }

    }
}

