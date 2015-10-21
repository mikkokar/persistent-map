# persistent-map
Persistent map data structure for Java

This is a persistent map data structure for Java. The implementation uses a hash array mapped
trie as an underlying data structure, and it uses bitmasks to compress trie's internal hash-array
nodes.

It supports all the basic operations:

  - Addition of new elements:
  
  ```
  PersistentMap<String, String> v1 = new PersistentMap<>();
  PersistentMap<String, String> v2 = v1.put("foo", "this");
  PersistentMap<String, String> v3 = v2.put("bar", "that");
  ```
  
  - Removal of elements:
  
  ```
  PersistentMap<String, String> v4 = v3.remove("foo");
  ```
  
  - Retrieving elements:
  
  ```
  String barVal = v4.get("bar")
  ```
  
See the PersistentMapTest.java for usage examples.  
  
