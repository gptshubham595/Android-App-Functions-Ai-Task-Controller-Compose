package com.shubham.todoAgent.core.appfunctions

import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import com.google.gson.*
import java.lang.reflect.Type

class AppFunctionDataTypeMetadataAdapter :
    JsonSerializer<AppFunctionDataTypeMetadata>,
    JsonDeserializer<AppFunctionDataTypeMetadata> {

    companion object {
        private const val CLASSNAME = "DATATYPE_CLASS"
    }

    override fun serialize(
        src: AppFunctionDataTypeMetadata,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = context.serialize(src)
        if (json.isJsonObject) json.asJsonObject.addProperty(CLASSNAME, src.javaClass.name)
        return json
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AppFunctionDataTypeMetadata {
        val obj = json.asJsonObject
        val className = obj.get(CLASSNAME).asString
        obj.remove(CLASSNAME)

        val clazz = Class.forName(className)
        return context.deserialize(obj, clazz)
    }
}