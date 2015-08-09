/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.api.exceptions;

import java.io.File;

public class FileNotFoundException extends RuntimeException {

	public FileNotFoundException(File file, Throwable e) {
		super(String.format("File %s not found.", file.getAbsolutePath()), e);
	}
	
	private static final long serialVersionUID = 1718559297558833658L;

}
