/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016. Diorite (by Bartłomiej Mazur (aka GotoFinal))
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.diorite.inject.impl.utils;

import org.diorite.inject.Controller;
import org.diorite.inject.InjectableClass;
import org.diorite.inject.Injection;
import org.diorite.inject.Injector;
import org.diorite.inject.Qualifiers;

import net.bytebuddy.description.type.TypeDescription;

public final class Constants
{
    public static final TypeDescription.ForLoadedType CONTROLLER        = new TypeDescription.ForLoadedType(Controller.class);
    public static final TypeDescription.ForLoadedType INJECTABLE_CLASS  = new TypeDescription.ForLoadedType(InjectableClass.class);
    public static final TypeDescription.ForLoadedType INJECTION_LIBRARY = new TypeDescription.ForLoadedType(Injection.class);
    public static final TypeDescription.ForLoadedType QUALIFIERS        = new TypeDescription.ForLoadedType(Qualifiers.class);
    public static final TypeDescription.ForLoadedType INSTANCE_SUPPLIER = new TypeDescription.ForLoadedType(InstanceSupplier.class);
    public static final TypeDescription.ForLoadedType INJECTOR          = new TypeDescription.ForLoadedType(Injector.class);

    private Constants()
    {
    }
}
