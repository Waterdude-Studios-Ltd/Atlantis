import groovy.namespace.QName
import groovy.util.Node

fun Node.versions(): List<String> = elements("version").map { it.text() }

fun Node.elements(name: String): List<Node> = depthFirst().filterIsInstance<Node>().filter { it.localName() == name }

fun Node.text(name: String): String? = children().filterIsInstance<Node>().firstOrNull { it.localName() == name }?.text()

fun Node.localName(): String = when (val name = name()) {
    is QName -> name.localPart
    else -> name.toString()
}
