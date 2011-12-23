package org.technbolts.jbehave.eclipse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IMethod;
import org.jbehave.core.parsers.RegexPrefixCapturingPatternParser;
import org.jbehave.core.parsers.StepMatcher;
import org.jbehave.core.steps.StepType;
import org.technbolts.util.ParametrizedString;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

/**
 * Candidate Step, prevent name clash with jbehave thus one uses potential instead.
 */
public class PotentialStep {
    public final IMethod method;
    public final IAnnotation annotation;
    public final StepType stepType;
    public final String stepPattern;
    private ParametrizedString parametrizedString;
    
    public PotentialStep(IMethod method, IAnnotation annotation, StepType stepType, String stepPattern) {
        super();
        this.method = method;
        this.annotation = annotation;
        this.stepType = stepType;
        this.stepPattern = stepPattern;
    }
    
    public float weightOf(String input) {
        return getParametrizedString().weightOf(input);
    }
    
    public ParametrizedString getParametrizedString() {
        if(parametrizedString==null)
            parametrizedString = new ParametrizedString(stepPattern);
        return parametrizedString;
    }
    
    public boolean hasVariable() {
        return getParametrizedString().getParameterCount()>0;
    }
    
    public boolean isTypeEqualTo(String searchedType) {
        return StringUtils.equalsIgnoreCase(searchedType, stepType.name());
    }
    
    public String fullStep() {
        return typeWord () + " " + stepPattern;
    }
    
    public String typeWord () {
        switch(stepType) {
            case WHEN: return "When";
            case THEN: return "Then";
            case GIVEN:
            default:
                return "Given";
        }
    }
    
    public boolean matches(String step) {
        return getMatcher(stepType, stepPattern).matches(step);
    }
    
    public String toString () {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(stepType).append("]").append(stepPattern);
        if(method==null) {
            builder.append(":n/a");
        }
        else {
            IClassFile classFile = method.getClassFile();
            if(classFile!=null)
                builder.append(classFile.getElementName());
            else
                builder.append("<classFile-unknown>");
            builder.append('#').append(method.getElementName());
        }
        return builder.toString();
    }
    
    private static RegexPrefixCapturingPatternParser stepParser = new RegexPrefixCapturingPatternParser();
    private static Cache<String, StepMatcher> matcherCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .maximumSize(50)
            .expireAfterWrite(10*60, TimeUnit.SECONDS)
            .build(
                new CacheLoader<String, StepMatcher>() {
                  public StepMatcher load(String key) throws Exception {
                      int indexOf = key.indexOf('/');
                      StepType stepType = StepType.valueOf(key.substring(0, indexOf));
                      String stepPattern = key.substring(indexOf+1);
                      return stepParser.parseStep(stepType, stepPattern);
                  }
                });
    
    public static StepMatcher getMatcher(StepType stepType, String stepPattern) {
        try {
            String key = stepType.name()+"/"+stepPattern;
            return matcherCache.get(key);
        } catch (ExecutionException e) {
            // rely on parse
            return stepParser.parseStep(stepType, stepPattern);
        }
    }

}