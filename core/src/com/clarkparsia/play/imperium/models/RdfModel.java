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

package com.clarkparsia.play.imperium.models;

import com.clarkparsia.play.imperium.jobs.SyncJob;
import com.clarkparsia.play.imperium.Imperium;

import play.db.jpa.JPASupport;
import play.db.jpa.Model;

import com.clarkparsia.empire.SupportsRdfId;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import java.net.URI;

/**
 * <p>Extends the normal Play JPA based model with hooks to commit changes to this object to an RDF-based
 * data source in addition to the normal SQL-backed database.</p>
 *
 * @author Michael Grove
 * @since 0.1
 */
@MappedSuperclass
public class RdfModel extends Model implements SupportsRdfId {

	/**
	 * Default support for managing and accessing the rdf:ID of this object
	 */
	@Column(name = "rdfid", nullable = false, unique=true)
	public String rdfid;

	/**
	 * @inheritDoc
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends JPASupport> T save() {
		try {
			if (Imperium.em().contains(this)) {
				Imperium.em().merge(this);
			}
			else {
				Imperium.em().persist(this);
			}
		}
		catch (Throwable theException) {
			theException.printStackTrace();
			// the changes to the underlying database failed, we need to schedule a job to synch the hibernate
			// database and the rdf database at a later date.
			SyncJob.schedule();
		}

		super.save();

		// this should be a safe cast
		return (T) this;
	}

	@Deprecated // exposed only to allow the sarge backwards migration.
	public <T extends JPASupport> T superSave() {
		return (T) super.save();
	}

	/**
	 * @inheritDoc
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends JPASupport> T delete() {
		try {
			Imperium.em().remove(this);
		}
		catch (Throwable theException) {
			// the changes to the underlying database failed, we need to schedule a job to synch the hibernate
			// database and the rdf database at a later date.
			SyncJob.schedule();
		}

		super.delete();

		// this should be a safe case
		return (T) this;
	}

	/**
	 * @inheritDoc
	 */
	public RdfKey getRdfId() {
		return rdfid == null ? null : new URIKey(URI.create(rdfid));
	}

	/**
	 * @inheritDoc
	 */
	public void setRdfId(final RdfKey theId) {
		if (theId == null) {
			rdfid = null;
		}
		else {
			rdfid = theId.toString();
		}
	}
}
