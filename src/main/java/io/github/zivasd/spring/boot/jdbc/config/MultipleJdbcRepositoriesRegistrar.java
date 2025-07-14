package io.github.zivasd.spring.boot.jdbc.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} to enable
 * {@link EnableMultipleJdbcRepositories} annotation.
 *
 * @author Jens Schauder
 */
class MultipleJdbcRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
    private @NonNull ResourceLoader resourceLoader;
    private @NonNull Environment environment;

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        super.setResourceLoader(resourceLoader);
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        super.setEnvironment(environment);
        this.environment = environment;
    }

    /*
     * 
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.config.
     * RepositoryBeanDefinitionRegistrarSupport#getAnnotation()
     */
    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableMultipleJdbcRepositories.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.config.
     * RepositoryBeanDefinitionRegistrarSupport#getExtension()
     */
    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new MultipleJdbcRepositoryConfigExtension();
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry,
            @NonNull BeanNameGenerator generator) {
        Assert.notNull(metadata, "AnnotationMetadata must not be null");
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");

        if (metadata.getAnnotationAttributes(getAnnotation().getName()) != null) {
            registerSingle(metadata, registry, generator);
        } else if (metadata.getAnnotationAttributes(EnableMultipleJdbcRepositoriesArray.class.getName()) != null) {
            registerMultiple(metadata, registry, generator);
        }
    }

    private void registerSingle(@NonNull AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry,
            @NonNull BeanNameGenerator generator) {
        AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
                metadata,
                getAnnotation(), resourceLoader, environment, registry, generator);

        RepositoryConfigurationExtension extension = getExtension();
        RepositoryConfigurationUtils.exposeRegistration(extension, registry, configurationSource);

        RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configurationSource,
                resourceLoader,
                environment);

        delegate.registerRepositoriesIn(registry, extension);
    }

    private void registerMultiple(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
            BeanNameGenerator generator) {
        MergedAnnotation<Annotation> annotation = metadata.getAnnotations().get(
                EnableMultipleJdbcRepositoriesArray.class.getName(),
                null, MergedAnnotationSelectors.firstDirectlyDeclared());

        MergedAnnotation<EnableMultipleJdbcRepositories>[] values = annotation.getAnnotationArray("value",
                EnableMultipleJdbcRepositories.class);
        for (MergedAnnotation<EnableMultipleJdbcRepositories> value : values) {
            AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
                    createAnnotationMetadataDelegate(metadata, value),
                    EnableMultipleJdbcRepositories.class, resourceLoader, environment, registry, generator);

            RepositoryConfigurationExtension extension = getExtension();
            RepositoryConfigurationUtils.exposeRegistration(extension, registry, configurationSource);

            RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configurationSource,
                    resourceLoader,
                    environment);

            delegate.registerRepositoriesIn(registry, extension);
        }
    }

    private AnnotationMetadata createAnnotationMetadataDelegate(AnnotationMetadata metadata,
            MergedAnnotation<EnableMultipleJdbcRepositories> annotation) {
        return (AnnotationMetadata) Proxy.newProxyInstance(metadata.getClass().getClassLoader(),
                new Class[] { AnnotationMetadata.class },
                new AnnotationMetadataDelegate(metadata, annotation));
    }

    static class AnnotationMetadataDelegate implements InvocationHandler {

        private final AnnotationMetadata instance;
        private final MergedAnnotation<EnableMultipleJdbcRepositories> annotation;

        public AnnotationMetadataDelegate(AnnotationMetadata instance,
                MergedAnnotation<EnableMultipleJdbcRepositories> annotation) {
            this.instance = instance;
            this.annotation = annotation;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getAnnotationAttributes".equals(method.getName())
                    || "getAllAnnotationAttributes".equals(method.getName())) {
                return annotation.asAnnotationAttributes(Adapt.ANNOTATION_TO_MAP);
            }
            return method.invoke(instance, args);
        }
    }

}
