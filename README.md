# apache-poi-bug57727
Demonstrates and fixes two bugs in XWPFParagraph.searchTest(..) method, 
see https://bz.apache.org/bugzilla/show_bug.cgi?id=57727

Go to the two test classes in `src/test/java/org.apache.poi.bugs.bug57727`:
* BeginCharTest
* YoyoPlayerTest

The jdoc there explains everything.

The fixed version of searchText() method is in `src/main/java/org.apache.poi.bugs.bug57727.FixedParagraphSearchText` class.
