package de.norm.events.slug

import org.springframework.modulith.ApplicationModule

/**
 * Module providing URL-friendly slug generation as a shared utility.
 *
 * This module has no dependencies on other application modules — it only
 * wraps the external [com.github.slugify.Slugify] library behind a
 * singleton ([SlugGenerator]) so all feature modules share a single instance.
 */
@ApplicationModule(allowedDependencies = [])
class SlugModule
