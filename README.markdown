# Imperium: Empire for Play!

Imperium is a plugin for the [Play! framework](http://playframework.org) similar to the existing JPA plugin that allows the use of [Empire](http://github.com/mhgrove/Empire) seamlessly in a Play! based application.

It provides two base module classes, RdfModel which is designed as a jumping off point for using both standard Hibernate based JPA and Empire in the same application with the same model.  When you persist your existing Hibernate backed beans to the database, they will also be persisted to your RDF store.  Also provided is EmpireModel which is an RDF-only base class for creating Play! model beans.

To use Imperium, just extend your model from one of the base models and include the plugin in your Play! application and you should be set.  You can access Imperium the same way as the JPA plugin, ala 'Imperium.em()'

Currently, this code for this plugin is for Play! 1.0.x, but it is the goal of the project to get it migrated to become an official Play! module.

## Configuring Imperium

In your standard Play! configuration, all properties prefixed 'empire.' will be grabbed by the plugin.  

"empire.support" is a comma separated list of the fully qualified class names of the Empire bindings (Jena, Sesame, 4Store, etc) to install when Empire initializes.  If none are provided, the Sesame bindings are the default.

All other 'empire' prefixed properties are included in the Empire global configuration passed to all Empire created EntityManager objects.  If no Empire configuration is found in the standard locations, Imperium will look in the application's conf directory for the Empire configuration.

## Questions, Comments and Concerns

Please refer to the [Empire mailing list](http://groups.google.com/group/empire-rdf) for help with Imperium.

## Licensing

Imperium is available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).