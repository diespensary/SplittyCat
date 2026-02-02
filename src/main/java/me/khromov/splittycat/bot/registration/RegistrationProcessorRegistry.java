package me.khromov.splittycat.bot.registration;

import me.khromov.splittycat.domain.entity.RegistrationStep;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RegistrationProcessorRegistry {
    private final Map<RegistrationStep, RegistrationStepProcessor> processors = new HashMap<>();

    public RegistrationProcessorRegistry(List<RegistrationStepProcessor> processors) {
        for (RegistrationStepProcessor processor : processors) {
            this.processors.put(processor.step(), processor);
        }
    }

    public RegistrationStepProcessor getProcessor(RegistrationStep step) {
        return processors.get(step);
    }
}
