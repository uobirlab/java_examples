java_examples
=============

You can find short little snippets of example code in this repository. The aim
is to collect small files which show off some way to use idiomatic rosjava.


Best practices
--------------

* Use a logger rather than 'System.out'. It's quieter, integrates with the ROS
  logging functionality, and allows you to filter based on severity and other
  traits.