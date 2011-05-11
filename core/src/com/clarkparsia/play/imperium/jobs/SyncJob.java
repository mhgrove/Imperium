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

package com.clarkparsia.play.imperium.jobs;

import play.jobs.Job;
import play.jobs.JobsPlugin;
import play.Play;
import play.PlayPlugin;
import play.Logger;
import play.db.jpa.JPA;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.lang.reflect.Modifier;

import com.clarkparsia.play.imperium.models.RdfModel;
import com.clarkparsia.play.imperium.EmpirePlugin;

import com.clarkparsia.play.imperium.Imperium;

/**
 * <p>Simple Job to synchronize the data from the normal relational database to the RDF database.</p>
 *
 * @author Michael Grove
 * @since 0.1
 * @version 0.1
 */
public class SyncJob extends Job {
	private static Lock mLock = new ReentrantLock();
	private static boolean mScheduled = false;

	public static void schedule() {
		mLock.lock();

		if (!mScheduled) {

			SyncJob aJob = new SyncJob();

			// TODO: can this just be done with new SyncJob.in(60 * 1000); ??
			JobsPlugin.executor.schedule((Callable) aJob, 60, TimeUnit.SECONDS);

			aJob.executor = JobsPlugin.executor;

			mScheduled = true;
		}

		mLock.unlock();
	}

	@Override
	public void doJob() {
		Logger.info("Starting SyncJob");

		// let other sync jobs get scheduled now that we are running one
		mLock.lock();

		mScheduled = false;

		mLock.unlock();

		List<Class> classesToSynch = Play.classloader.getAssignableClasses(RdfModel.class);

		for (Class aClass : classesToSynch) {
			if (RdfModel.class.isAssignableFrom(aClass) && !aClass.isInterface() && !Modifier.isAbstract(aClass.getModifiers())) {
				try {
					List aList = (List) aClass.asSubclass(RdfModel.class).getMethod("findAll").invoke(null);

					for (Object aObj : aList) {
						RdfModel aModel = (RdfModel) aObj;

						try {
							Imperium.em().remove(aModel);
						}
						catch (IllegalArgumentException e) {
							// if it doesn't exist, that's ok, we were removing it anyway
						}

						Imperium.em().persist(aModel);

						// persist the rdfid of these things
						aModel.save();
					}
				}
				catch (Exception e) {
					e.printStackTrace();

					Imperium.em().getTransaction().rollback();

					throw new RuntimeException(e);
				}
			}
		}

		Logger.info("Completing SyncJob");

		JPA.em().flush();

		// TODO: do we need to lock the db while doing the sync since it is effectively a wipe and load?

	}
}
