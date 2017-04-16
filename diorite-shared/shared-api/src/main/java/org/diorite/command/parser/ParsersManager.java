/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017. Diorite (by Bartłomiej Mazur (aka GotoFinal))
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

package org.diorite.command.parser;

import java.util.Collection;
import java.util.function.Function;

import org.diorite.command.Argument;
import org.diorite.command.parser.basic.StringParser;

/**
 *
 */
public interface ParsersManager
{
    /**
     * Create context for given input data and collection of requested arguments.
     *
     * @param data
     *         raw string input.
     * @param arguments
     *         collection of requested arguments.
     *
     * @return created parser context.
     */
    ParserContext createContext(String data, Collection<? extends Argument<?>> arguments);
//
//    default <E> CustomArgumentBuilder<String, E> createParser(String name, Class<E> to)
//    {
//        return this.createParser(this.getParser(StringParser.class), to, parser);
//    }
//
//    <T, E> CustomArgumentBuilder<T, E> createParser(TypeParser<T> from, Class<E> to, Function<T, E> parser);
}