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

import com.clarkparsia.empire.SupportsRdfId;
import com.clarkparsia.empire.annotation.SupportsRdfIdImpl;

import com.clarkparsia.play.imperium.Imperium;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.Serializable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.Collections;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import play.data.binding.BeanWrapper;

import play.data.validation.Validation;

import play.exceptions.UnexpectedException;

import javax.persistence.OneToOne;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.ManyToMany;

/**
 * <p>Base Play! model for using Empire for persistence.  Though Empire uses JPA, extending from JPAModel won't work
 * because a lot of the JPA support in Play uses JPQL, which Empire does not yet support.  So functionality
 * provided by JPA support will be re-created here.</p>
 *
 * @author Michael Grove
 * @since 0.1
 * @version 0.1
 */
public abstract class EmpireModel implements Serializable, SupportsRdfId {

	/**
	 * Default implementation for having an rdf:ID
	 */
	private SupportsRdfId mIdSupport = new SupportsRdfIdImpl();

	/**
	 * Save this model to the database
	 * @param <T> the type of the model
	 * @return this model
	 */
	@SuppressWarnings("unchecked")
	public <T extends EmpireModel> T save() {
		if (Imperium.em().contains(this)) {
			Imperium.em().merge(this);
		}
		else {
			Imperium.em().persist(this);
		}

		// this should be a safe cast
		return (T) this;
	}

	/**
	 * Remove this model from the database
	 * @param <T> the type of the model
	 * @return this model
	 */
	@SuppressWarnings("unchecked")
	public <T extends EmpireModel> T delete() {
		Imperium.em().remove(this);

		// this should be a safe cast
		return (T) this;
	}

	/**
	 * @inheritDoc
	 */
	public RdfKey getRdfId() {
		return mIdSupport.getRdfId();
	}

	/**
	 * @inheritDoc
	 */
	public void setRdfId(final RdfKey theId) {
		mIdSupport.setRdfId(theId);
	}

	/**
	 * Return all objects of this type from the database
	 * @param <T> the type to return
	 * @return all objects of this type in the database
	 */
	public abstract <T extends EmpireModel> Collection<T> all();

	/**
	 * Apply the edits to the bean
	 * @return the edited bean
	 */
	public <T> T edit(String theName, Map<String, String[]> theParams) {
		try {
			BeanWrapper aBeanWrapper = new BeanWrapper(getClass());

			Set<Field> aFieldList = Sets.newHashSet();
			Class aClass = this.getClass();

			while (!aClass.equals(EmpireModel.class)) {
				Collections.addAll(aFieldList, aClass.getDeclaredFields());
				aClass = aClass.getSuperclass();
			}

			for (Field aField : aFieldList) {
				boolean isEntity = false;
				boolean multiple = false;

				if (aField.isAnnotationPresent(OneToOne.class) || aField.isAnnotationPresent(ManyToOne.class)) {
					isEntity = true;
				}
				if (aField.isAnnotationPresent(OneToMany.class) || aField.isAnnotationPresent(ManyToMany.class)) {
					isEntity = true;
					multiple = true;
				}

				if (isEntity) {
					if (multiple && Collection.class.isAssignableFrom(aField.getType())) {
						Collection aCollection = Lists.newArrayList();

						if (Set.class.isAssignableFrom(aField.getType())) {
							aCollection = Sets.newHashSet();
						}

						String[] aIds = theParams.get(theName + "." + aField.getName() + "@id");
						if (aIds == null) {
							aIds = theParams.get(theName + "." + aField.getName() + ".id");
						}

						if (aIds != null) {
							theParams.remove(theName + "." + aField.getName() + ".id");
							theParams.remove(theName + "." + aField.getName() + "@id");

							for (String aId : aIds) {

								if (aId.equals("")) {
									continue;
								}
								
								Class aFieldType = (Class) ((ParameterizedType) aField.getGenericType()).getActualTypeArguments()[0];
								
								Object aResult = Imperium.em().find(aFieldType, aId);
								
								if (aResult != null) {
									aCollection.add(aResult);
								}
								else {
									Validation.addError(theName+"."+aField.getName(), "validation.notFound", aId);
								}
							}

							aBeanWrapper.set(aField.getName(), this, aCollection);
						}
					}
					else {
						String[] aIds = theParams.get(theName + "." + aField.getName() + "@id");

						if (aIds == null) {
							aIds = theParams.get(theName + "." + aField.getName() + ".id");
						}

						if (aIds != null && aIds.length > 0 && !aIds[0].equals("")) {
							theParams.remove(theName + "." + aField.getName() + ".id");
							theParams.remove(theName + "." + aField.getName() + "@id");
							
							Object aValue = Imperium.em().find(aField.getType(), aIds[0]);
							
							if (aValue != null) {
								aBeanWrapper.set(aField.getName(), this, aValue);
							} 
							else {
								Validation.addError(theName+"."+aField.getName(), "validation.notFound", aIds[0]);
							}
						}
						else if (aIds != null && aIds.length > 0 && aIds[0].equals("")) {
							aBeanWrapper.set(aField.getName(), this , null);

							theParams.remove(theName + "." + aField.getName() + ".id");
							theParams.remove(theName + "." + aField.getName() + "@id");
						}
					}
				}
			}

			aBeanWrapper.bind(theName, this.getClass(), theParams, "", this);

			return (T) this;
		}
		catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}
}
