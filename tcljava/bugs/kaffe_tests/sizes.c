#include <stdio.h>


union jvalue {
  char          z;
  char		b;
  char		c; 
  short		s;
  int		i;
  long		j;
  float		f;
  double        d;
  void*		l;
};

typedef union jvalue jvalue;


struct _slots {
        union {
                int            tint;
                long           tword;
                long           tlong;
                float          tfloat;
                double         tdouble;
                void*           taddr;
                char*           tstr;
        } v;
};

typedef struct _slots slots;



void main(int argc, char ** argv) {
  jvalue var;
  slots val;
  slots* ref;


  var.l = 0;
  val.v.tint = 1;
  ref = (slots *) &var;

  ref[0].v.tint =  val.v.tint;

  if (ref[0].v.tint) {
    printf("ref[0].v.tint is \"%d\"\n", ref[0].v.tint);
  } else {
    printf("ref[0].v.tint is 0\n");
  }

  if (var.z) {
    printf("var.z is \"%d\"\n", ((int) var.z));
  } else {
    printf("var.z is 0\n");
  }
}
