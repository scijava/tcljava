import java.util.zip.*;

public class ZipExtraComments {
    public static void main(String[] argv) throws Exception {
	ZipEntry ze = new ZipEntry("empty");
	ze.setExtra(null);
	ze.setComment(null);
	System.out.println("OK");
    }
}


/*
JDK 

% java ZipExtraComments
OK

*/



/*
Kaffe

kaffe ZipExtraComments
java.lang.NullPointerException
        at java.util.zip.ZipEntry.setExtra(ZipEntry.java:129)
        at ZipExtraComments.main(ZipExtraComments.java:6)

*/


/*
Kaffe with my patch

% kaffe ZipExtraComments
OK
*/



/*
PATCH

Index: ZipEntry.java
===================================================================
RCS file: /home/cvspublic/kaffe/libraries/javalib/java/util/zip/ZipEntry.java,v
retrieving revision 1.5
diff -u -r1.5 ZipEntry.java
--- ZipEntry.java       1999/02/13 09:02:58     1.5
+++ ZipEntry.java       1999/02/16 07:51:43
@@ -124,12 +124,12 @@
     return (method);
   }
 
-  public void setExtra(byte xtra[])
+  public void setExtra(byte extra[])
   {
-    if (xtra.length > 0xFFFFF) {
+    if ((extra != null) && (extra.length > 0xFFFFF)) {
       throw new IllegalArgumentException("extra length > 0xFFFFF");
     }
-    extra = xtra;
+    this.extra = extra;
   }
 
   public byte[] getExtra()
@@ -137,12 +137,12 @@
     return (extra);
   }
 
-  public void setComment(String commnt)
+  public void setComment(String comment)
   {
-    if (commnt.length() > 0xFFFF) {
+    if ((comment != null) && (comment.length() > 0xFFFF)) {
       throw new IllegalArgumentException("comment length > 0xFFFF");
     }
-    comment = commnt;
+    this.comment = comment;
   }
 
   public String getComment()

*/
