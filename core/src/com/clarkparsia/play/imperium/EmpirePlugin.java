/*
 * Copyright (c) 2009-2011 Clark & Parsia, LLC. <http://www.clarkparsia.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clarkparsia.play.imperium;

import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClassloader;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.EmpireException;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.config.ConfigKeys;
import com.clarkparsia.empire.config.io.ConfigReader;
import com.clarkparsia.empire.config.io.impl.PropertiesConfigReader;
import com.clarkparsia.empire.config.io.impl.XmlConfigReader;
import com.clarkparsia.empire.util.EmpireModule;

import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

/**
 * <p>Initialize Empire support for Play! applications.</p>
 *
 * @author Michael Grove
 * @since 0.1
 * @version 0.1
 */
public class EmpirePlugin extends PlayPlugin {

	public static boolean AUTO_TX = System.getProperty("imperium.autotx") != null ? System.getProperty("imperium.autotx").equalsIgnoreCase("true") : true;

	/**
	 * @inheritDoc
	 */
	@Override
	public void onApplicationStart() {
		// TODO: consider moving this to the onConfigurationRead event
		super.onApplicationStart();

		Thread.currentThread().setContextClassLoader(Play.classloader);

		Map<String, String> aConfig = new HashMap<String, String>();

		for (Object aObj : Play.configuration.keySet()) {
			String aKey = aObj.toString();

			if (aKey.startsWith("empire.")) {
				aConfig.put(aKey.substring(7), Play.configuration.getProperty(aKey));
			}
		}

		Collection<EmpireModule> aModules = new ArrayList<EmpireModule>();

		if (aConfig.containsKey("support")) {
			String aSupport = aConfig.get("support");

			for (String aClassName : aSupport.split("(,\\w)")) {
				try {
					aModules.add( (EmpireModule) Class.forName(aClassName.trim()).newInstance());
				}
				catch (ClassCastException e) {
					play.Logger.error(e, "You must specify a class which is a compatible Empire Guice Module.");
				}
				catch (Exception e) {
					play.Logger.error(e, "Error while loading a support class.");
				}
			}
		}

		if (aModules.isEmpty()) {
			// sesame will be the default
			aModules.add(new OpenRdfEmpireModule());
		}

		Map<String, String> aGlobalConfig = new HashMap<String, String>();
//		aGlobalConfig.put(ConfigKeys.ANNOTATION_INDEX, "empire.config");

		if (!isEmpireConfigFound()) {
			EmpireConfiguration aEmpireConfig = new EmpireConfiguration(aGlobalConfig,
													Collections.singletonMap("imperium", aConfig));

			try {
				if (Play.configuration.containsKey("empire.config")) {
					String aPath = Play.configuration.getProperty("empire.config");

					ConfigReader aReader = new PropertiesConfigReader();
					if (aPath.endsWith("xml")) {
						aReader = new XmlConfigReader();
					}

					aEmpireConfig = aReader.read(new FileInputStream(aPath));
				}
				else if (Play.getVirtualFile("conf/empire.config.properties") != null &&
						 Play.getVirtualFile("conf/empire.config.properties").exists()) {
					aEmpireConfig = new PropertiesConfigReader().read(new FileInputStream(Play.getVirtualFile("conf/empire.config.properties").getRealFile()));
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				play.Logger.error("empire config load error", e);
			}
			catch (EmpireException e) {
				play.Logger.error("empire config load error", e);
			}
			
			Empire.init(aEmpireConfig,
						aModules.toArray(new EmpireModule[aModules.size()]));
		}
		else {
			Empire.init(aModules.toArray(new EmpireModule[aModules.size()]));
		}
	}

	/**
	 * Return whether or not there is a local Empire config file available
	 * @return true if there is a config file, false otherwise
	 */
	private boolean isEmpireConfigFound() {
		// TODO: refer to something in Empire core that determins this rather than having this code copied from
		// DefaultEmpireModule

		File aConfigFile = null;

		// not ideal, really we want just a single standard config file name with the system property which can override
		// that.  but since we don't have a standard yet, we'll check a bunch of them.
		if (System.getProperty("empire.configuration.file") != null && new File(System.getProperty("empire.configuration.file")).exists()) {
			aConfigFile = new File(System.getProperty("empire.configuration.file"));
		}
		else if (new File("empire.config").exists()) {
			aConfigFile = new File("empire.config");
		}
		else if (new File("empire.properties").exists()) {
			aConfigFile = new File("empire.properties");
		}
		else if (new File("empire.config.properties").exists()) {
			aConfigFile = new File("empire.config.properties");
		}
		else if (new File("empire.xml").exists()) {
			aConfigFile = new File("empire.xml");
		}
		else if (new File("empire.config.xml").exists()) {
			aConfigFile = new File("empire.config.xml");
		}

		return aConfigFile != null;
	}

	/**
	 * @inheritDoc
	 */
    @Override
    public void beforeInvocation() {
        startTx();
    }

	/**
	 * @inheritDoc
	 */
    @Override
    public void afterInvocation() {
        closeTx(false);
    }

	/**
	 * @inheritDoc
	 */
    @Override
    public void afterActionInvocation() {
        closeTx(false);
    }

	/**
	 * @inheritDoc
	 */
    @Override
    public void onInvocationException(Throwable e) {
		e.printStackTrace();
        closeTx(true);
    }

	/**
	 * @inheritDoc
	 */
    @Override
    public void invocationFinally() {
        closeTx(true);
    }

    /**
     * Start a transaction
     */
    public static void startTx() {
        if (AUTO_TX) {
			if (Imperium.em().getTransaction() != null && Imperium.em().getTransaction().isActive()) {
				Imperium.em().joinTransaction();
			}
			else {
            	Imperium.em().getTransaction().begin();
			}
        }
    }

    /**
     * Close the current transaction
     * @param theRollback if current transaction be committed (false) or cancelled (true)
     */
    public static void closeTx(boolean theRollback) {
        if (Imperium.get() == null) {
            return;
        }

        EntityManager aManager = Imperium.em();

		if (AUTO_TX) {
			if (aManager.getTransaction().isActive()) {
				if (theRollback || aManager.getTransaction().getRollbackOnly()) {
					aManager.getTransaction().rollback();
				}
				else {
					try {
						if (AUTO_TX) {
							aManager.getTransaction().commit();
						}
					}
					catch (Throwable e) {
						for (int i = 0; i < 10; i++) {
							if (e instanceof PersistenceException && e.getCause() != null) {
								e = e.getCause();
								break;
							}

							e = e.getCause();
							if (e == null) {
								break;
							}
						}
						throw new ImperiumException("Cannot commit", e);
					}
				}
			}
		}
	}
}
