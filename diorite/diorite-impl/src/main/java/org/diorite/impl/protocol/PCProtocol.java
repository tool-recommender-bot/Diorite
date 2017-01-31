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

package org.diorite.impl.protocol;

import javax.annotation.Nullable;

import org.diorite.impl.protocol.p16w50a.PCp16w50a;
import org.diorite.DioriteConfig;
import org.diorite.core.protocol.Protocol;

public class PCProtocol extends Protocol<PCProtocolVersion>
{
    @Nullable private PCProtocolVersion newest;

    public PCProtocol(DioriteConfig config)
    {
        super("pc");
        this.addVersion(new PCp16w50a(this, config));
    }

    @Override
    public void addVersion(PCProtocolVersion version)
    {
        super.addVersion(version);
        if ((this.newest == null) || ((version.getVersion() > this.newest.getVersion()) && (version.isStable() || ! this.newest.isStable())))
        {
            this.newest = version;
        }
    }

    @Override
    public PCProtocolVersion getDefault()
    {
        if (this.newest == null)
        {
            throw new IllegalStateException("Missing default version.");
        }
        return this.newest;
    }
}