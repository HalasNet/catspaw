package org.catspaw.cherubim.persistence.spring.hibernate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.persistence.Entity;

import org.apache.commons.beanutils.MethodUtils;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.util.ClassUtils;

/**
 * Created on: 2007/11/24
 * @author Alan She
 */
public class ClasspathScanningAnnotationSessionFactoryBean extends
		AnnotationSessionFactoryBean {

	private static final String		DEFAULT_RESOURCE_PATTERN	= "**/*.class";
	private ResourcePatternResolver	resourcePatternResolver		= new PathMatchingResourcePatternResolver();
	private MetadataReaderFactory	metadataReaderFactory		= new CachingMetadataReaderFactory(
																		this.resourcePatternResolver);
	private final TypeFilter		entityFilter				= new AnnotationTypeFilter(
																		Entity.class);
	private String					resourcePattern				= DEFAULT_RESOURCE_PATTERN;
	private String[]				basePackages;

	public void setBasePackages(String[] basePackages) {
		this.basePackages = basePackages;
	}

	protected void postProcessAnnotationConfiguration(Configuration config)
			throws HibernateException {
		for (String basePackage : basePackages) {
			try {
				String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
						+ ClassUtils
								.convertClassNameToResourcePath(basePackage)
						+ "/" + this.resourcePattern;
				Resource[] resources = this.resourcePatternResolver
						.getResources(packageSearchPath);
				for (int i = 0; i < resources.length; i++) {
					Resource resource = resources[i];
					MetadataReader metadataReader = this.metadataReaderFactory
							.getMetadataReader(resource);
					if (isEntity(metadataReader)) {
						String classFileFullPath = resource.getURL().getPath();
						String basePackageResourcePath = ClassUtils
								.convertClassNameToResourcePath(basePackage);
						int startIndex = classFileFullPath
								.indexOf(basePackageResourcePath);
						final String classFilePath = classFileFullPath
								.substring(startIndex, classFileFullPath
										.length()
										- ClassUtils.CLASS_FILE_SUFFIX.length());
						Class<?> entityClass = null;
						ClassLoader classLoader = Thread.currentThread()
								.getContextClassLoader();
						if (classLoader == null) {
							classLoader = ClasspathScanningAnnotationSessionFactoryBean.class
									.getClassLoader();
						}
						try {
							entityClass = ClassUtils
									.forName(
												ClassUtils
														.convertResourcePathToClassName(classFilePath),
												classLoader);
						} catch (ClassNotFoundException e) {
							throw new HibernateException(
									"Entity class not found during classpath scanning",
									e);
						}
						MethodUtils.invokeMethod(config, "addAnnotatedClass",
													entityClass);
					}
				}
			} catch (IOException ex) {
				throw new HibernateException(
						"I/O failure during classpath scanning", ex);
			} catch (NoSuchMethodException e) {
				//ignored
			} catch (IllegalAccessException e) {
				//ignored
			} catch (InvocationTargetException e) {
				//ignored
			}
		}
	}

	private boolean isEntity(MetadataReader metadataReader) throws IOException {
		if (entityFilter.match(metadataReader, this.metadataReaderFactory)) {
			return true;
		}
		return false;
	}
}
