/*
 * Copyright 2026 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apitomy.hub.api.codegen.refs;

/**
 * Thrown when a $ref to a local file cannot be resolved (missing file, unparseable content,
 * missing fragment, or a file outside the allowed directory).
 *
 * @author Matheus Cruz
 */
public class ExternalReferenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExternalReferenceException(String message) {
        super(message);
    }

    public ExternalReferenceException(String message, Throwable cause) {
        super(message, cause);
    }

}
