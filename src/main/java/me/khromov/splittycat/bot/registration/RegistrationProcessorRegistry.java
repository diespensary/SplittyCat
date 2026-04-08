package me.khromov.splittycat.bot.registration;

import me.khromov.splittycat.domain.entity.RegistrationStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RegistrationProcessorRegistry {
    private final Map<RegistrationStep, RegistrationStepProcessor> processors;

    public RegistrationProcessorRegistry(List<RegistrationStepProcessor> processors) {
        this.processors = processors.stream()
                .collect(Collectors.toUnmodifiableMap(RegistrationStepProcessor::step, Function.identity()));
    }

    public RegistrationStepProcessor getProcessor(RegistrationStep step) {
        return processors.get(step);
    }
}
