/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

private const val prefix = "kotlinx.serialization.modules.SerialNameCollisionTest"

class SerialNameCollisionTest {

    // Polymorphism
    interface IBase

    @Serializable
    abstract class Base : IBase

    @Serializable
    data class Derived(val type: String, val type2: String) : Base()

    @Serializable
    data class DerivedCustomized(
        @SerialName("type") val t: String, @SerialName("type2") val t2: String, val t3: String
    ) : Base()

    @Serializable
    @SerialName("$prefix.Derived")
    data class DerivedRenamed(val type: String, val type2: String) : Base()

    private fun Json(discriminator: String, context: SerialModule, useArrayPolymorphism: Boolean = false) = Json(
        configuration = JsonConfiguration.Stable.copy(
            classDiscriminator = discriminator,
            useArrayPolymorphism = useArrayPolymorphism
        ), context = context
    )

    @Test
    fun testCollisionWithDiscriminator() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                Derived::class with Derived.serializer()
            }
        }

        assertFailsWith<IllegalArgumentException> { Json("type", module) }
        assertFailsWith<IllegalArgumentException> { Json("type2", module) }
        Json("type3", module) // OK
    }

    @Test
    fun testNoCollisionWithArrayPolymorphism() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                Derived::class with Derived.serializer()
            }
        }
        Json("type", module, true)
    }

    @Test
    fun testCollisionWithDiscriminatorViaSerialNames() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                DerivedCustomized::class with DerivedCustomized.serializer()
            }
        }

        assertFailsWith<IllegalArgumentException> { Json("type", module) }
        assertFailsWith<IllegalArgumentException> { Json("type2", module) }
        assertFailsWith<IllegalArgumentException> { Json("t3", module) }
        Json("t4", module) // OK

    }

    @Test
    fun testCollisionWithinHierarchy() {
        SerializersModule {
            assertFailsWith<IllegalArgumentException> {
                polymorphic(Base::class) {
                    Derived::class with Derived.serializer()
                    DerivedRenamed::class with DerivedRenamed.serializer()
                }
            }
        }
    }

    @Test
    fun testCollisionWithinHierarchyViaConcatenation() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                Derived::class with Derived.serializer()
            }
        }
        val module2 = SerializersModule {
            polymorphic(Base::class) {
                DerivedRenamed::class with DerivedRenamed.serializer()
            }
        }

        assertFailsWith<IllegalArgumentException> { module + module2 }
    }

    @Test
    fun testNoCollisionWithinHierarchy() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                Derived::class with Derived.serializer()
            }

            polymorphic(IBase::class) {
                DerivedRenamed::class with DerivedRenamed.serializer()
            }
        }

        assertSame(Derived.serializer(), module.getPolymorphic(Base::class, "$prefix.Derived"))
        assertSame(
            DerivedRenamed.serializer(),
            module.getPolymorphic(IBase::class, "$prefix.Derived")
        )
    }
}
