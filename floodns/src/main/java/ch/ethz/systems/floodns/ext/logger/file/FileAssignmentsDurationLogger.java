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

package ch.ethz.systems.floodns.ext.logger.file;

import ch.ethz.systems.floodns.core.Simulator;
import ch.ethz.systems.floodns.deeplearningtraining.AssignmentsDurationLogger;
import ch.ethz.systems.floodns.ext.routing.CentralizedRoutingStrategy;

import java.io.IOException;
import java.io.Writer;

public class FileAssignmentsDurationLogger extends AssignmentsDurationLogger {

    private final Writer writerJobInfoFile;

    public FileAssignmentsDurationLogger(Simulator simulator, CentralizedRoutingStrategy routingStrategy, Writer writerJobInfoFile) {
        super(simulator, routingStrategy);
        this.writerJobInfoFile = writerJobInfoFile;
    }

    /**
     * Save the general (statistical) information of the flow.
     * Values are comma-separated, lines are new-line-separated.
     * <p>
     * File format (2 columns):
     * [averageDuration],[numberOfConnections]
     * <p>
     */
    @Override
    public void saveInfo(double averageDuration, int numConnections) {
        try {
            writerJobInfoFile.write(averageDuration + "," + numConnections + ",\r\n");
            writerJobInfoFile.flush();
        } catch (IOException e) {
            throw new FatalLogFileException(e);
        }
    }

}
