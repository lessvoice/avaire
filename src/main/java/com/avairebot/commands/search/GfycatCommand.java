package com.avairebot.commands.search;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.ThreadCommand;
import com.avairebot.factories.RequestFactory;
import com.avairebot.requests.Response;
import com.avairebot.requests.service.GfycatService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GfycatCommand extends ThreadCommand {

    public GfycatCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Gfycat Command";
    }

    @Override
    public String getDescription() {
        return "Returns a random gif for you from gfycat.com with the given query.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <query>` - Finds a random image with the given query");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command cats`");
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("gfycat", "gif");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("throttle:user,2,5");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "errors.missingArgument", "queue");
        }

        RequestFactory.makeGET("https://api.gfycat.com/v1test/gfycats/search")
            .addParameter("count", 25)
            .addParameter("search_text", String.join(" ", args))
            .send((Consumer<Response>) response -> {
                String url = getUrlFromResponse(response);

                if (url == null) {
                    context.makeError(context.i18n("noResults"))
                        .set("query", String.join(" ", args))
                        .queue();

                    return;
                }

                context.makeEmbeddedMessage().setImage(url).queue();
            });
        return true;
    }

    private String getUrlFromResponse(Response response) {
        GfycatService gfyCat = (GfycatService) response.toService(GfycatService.class);
        if (gfyCat == null) {
            return null;
        }

        for (int i = 0; i < 5; i++) {
            Map<String, Object> item = gfyCat.getRandomGfycatsItem();
            if (item == null) {
                break;
            }

            if (isValidUrl((String) item.get("gifUrl"))) {
                return (String) item.get("gifUrl");
            }
        }
        return null;
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://"));
    }
}
