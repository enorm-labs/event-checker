package de.norm.events

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventsImporterApplication

fun main(args: Array<String>) {
    runApplication<EventsImporterApplication>(*args)
}
