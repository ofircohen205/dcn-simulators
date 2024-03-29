/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 snkas
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

package ch.ethz.systems.floodns.ext.basicsim.schedule;

public class ConnectionScheduleEntry {

    private final long connectionId;
    private final int fromNodeId;
    private final int toNodeId;
    private final long sizeByte;
    private final long startTimeNs;
    private final String additionalParameters;
    private final String metadata;

    public ConnectionScheduleEntry(long connectionId, int fromNodeId, int toNodeId, long sizeByte, long startTimeNs, String additionalParameters, String metadata) {
        this.connectionId = connectionId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.sizeByte = sizeByte;
        this.startTimeNs = startTimeNs;
        this.additionalParameters = additionalParameters;
        this.metadata = metadata;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public int getFromNodeId() {
        return fromNodeId;
    }

    public int getToNodeId() {
        return toNodeId;
    }

    public long getSizeByte() {
        return sizeByte;
    }

    public long getStartTimeNs() {
        return startTimeNs;
    }

    public String getAdditionalParameters() {
        return additionalParameters;
    }

    public String getMetadata() {
        return metadata;
    }

}
