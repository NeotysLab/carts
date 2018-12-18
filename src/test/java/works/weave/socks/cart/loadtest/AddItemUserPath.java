package works.weave.socks.cart.loadtest;

import com.google.common.collect.ImmutableList;
import com.neotys.neoload.model.repository.*;
import com.neotys.testing.framework.BaseNeoLoadDesign;
import com.neotys.testing.framework.BaseNeoLoadUserPath;

import java.util.List;

import static java.util.Collections.emptyList;


public class AddItemUserPath extends BaseNeoLoadUserPath {
    public AddItemUserPath(BaseNeoLoadDesign design) {
        super(design);
    }

    @Override
    public UserPath createVirtualUser(BaseNeoLoadDesign baseNeoLoadDesign) {
        final Server server = baseNeoLoadDesign.getServerByName("Carts_Server");

        final String jsonPayload=" {\n" +
                "    \"itemId\":\"03fef6ac-1896-4ce8-bd69-b798f85c6e0b\",\n" +
                "    \"unitPrice\":\"99.99\"\n" +
                " }";
        final List<Header> headerList= ImmutableList.of(
                header("Cache-Control","no-cache"),
                header("Content-Type","application/json"),
                header("json","true")
        );
        final PostRequest postRequest = postTextBuilderWithHeaders(server,headerList,"/carts/1/items",jsonPayload,emptyList(),emptyList()).build();

        final Delay delay = thinkTime(250);
        final ImmutableContainerForMulti actionsContainer = actionsContainerBuilder()
                .addChilds(container("AddItem", postRequest, delay))
                .build();

        return userPathBuilder("AddItemToCart")
                .actionsContainer(actionsContainer)
                .build();
    }
}
