package org.mirgor.console_agent.service.request_builder;

import org.mirgor.console_agent.service.model.Model;
import org.springframework.stereotype.Component;

import java.util.List;
//TODO fix request
@Component
public class ClaudeRequestBuilder extends ModelRequestBuilder{

    @Override
    public List<Model> getModels() {
        return List.of(Model.CLAUDE_SONNET_3_7);
    }

}
