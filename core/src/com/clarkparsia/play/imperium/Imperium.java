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

import com.clarkparsia.empire.Empire;

import javax.persistence.EntityManager;

import javax.persistence.PersistenceContext;

/**
 * <p>Component to provide thread local access to an {@link EntityManager}.</p>
 *
 * @author Michael Grove
 * @since 0.1
 * @version 0.1
 */
public class Imperium {
	/**
	 * A thread-local reference to an instance of Empire
	 */
	private static ThreadLocal<Imperium> mLocalInst = new ThreadLocal<Imperium>();

	/**
	 * The current EntityManager
	 */
	@PersistenceContext(name="imperium")
	public EntityManager mEntityManager;

	/**
	 * Return whether or not Empire has been initialized for the local thread context
	 * @return true if it has been initialized, false otherwise
	 */
	public static boolean isInitialized() {
		return mLocalInst.get() != null;
	}

	/**
	 * Return the thread local entity manager
	 * @return the entity manager
	 */
	static Imperium get() {
		Imperium aEmpire = mLocalInst.get();
		
		if (aEmpire == null) {
			aEmpire = create();
		}

		return aEmpire;
	}

	/**
	 * Return the current Empire EntityManager
	 * @return the current EntityManager
	 */
	public static EntityManager em() {
		return get().getEntityManager();
	}

	/**
	 * Create an instance of Imperium using the persistence context injected into here via Empire.
	 * @return this instance
	 */
	private static Imperium create() {
		if (mLocalInst.get() != null) {
			mLocalInst.get().getEntityManager().close();

			mLocalInst.remove();
		}

		Imperium aEmpire = Empire.get().instance(Imperium.class);

		mLocalInst.set(aEmpire);

		return aEmpire;
	}

	/**
	 * Return the current {@link EntityManager}.
	 * @return the entity manager
	 */
	private EntityManager getEntityManager() {
		return mEntityManager;
	}

	/**
	 * Close Empire
	 */
	public static void close() {
		if (mLocalInst.get() != null) {
			if (mLocalInst.get().getEntityManager().isOpen()) {
				mLocalInst.get().getEntityManager().close();
			}

			mLocalInst.remove();
		}
	}
}
