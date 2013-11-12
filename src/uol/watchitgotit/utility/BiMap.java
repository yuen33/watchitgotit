/*
 * Copyright (C) 2010 Francesco Feltrinelli <francesco.feltrinelli@gmail.com>
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
 * 
 */

package uol.watchitgotit.utility;

import java.util.HashMap;

public class BiMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = 1690333017283993642L;
	
	public K getKey(V value){
		
		for (Entry<K, V> entry: entrySet())
			if (entry.getValue().equals(value))
				return entry.getKey();
		return null;
	}

}
