#include <stdio.h>


/*
big endian
*/

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

void printjbits(jvalue * ref);


void main(int argc, char ** argv) {
  jvalue var;
  char * ptmp;
  char ctmp;

  memset(&var, 0xFF, 8); /* turn on all 64 bits */

  var.z = 0;
  printjbits(&var);

  memset(&var, 0xFF, 8); /* turn on all 64 bits */

  var.z = 1;
  printjbits(&var);

  memset(&var, 0xFF, 8); /* turn on all 64 bits */

  var.i = 0;
  printjbits(&var);

  memset(&var, 0xFF, 8); /* turn on all 64 bits */

  var.i = 1;
  printjbits(&var);




  memset(&var, 0xFF, 8); /* turn on all 64 bits */

  var.i = 1;

  /* swap bytes */
  ptmp = (char *) &var.i;

  ctmp = *(ptmp); /* copy byte 1 to tmp */
  *(ptmp) = *(ptmp + 3); /* copy byte 4 over byte 1 */ 
  *(ptmp + 3) = ctmp;  /* copy tmp byte to byte 4 */
  ctmp = *(ptmp + 1); /* copy byte 2 to tmp */
  *(ptmp + 1) = *(ptmp + 2); /* copy byte 3 over byte 2 */ 
  *(ptmp + 2) = ctmp;  /* copy tmp byte to byte 3 */

  printjbits(&var);

}


void printjbits(jvalue * ref) {
  int* pvar;
  int i;

  printf("var.z is \"%d\"\n", ((int) ref->z));
/*
  printf("var.b is \"%d\"\n", ((int) ref->b));
  printf("var.c is \"%d\"\n", ((int) ref->c));
*/
  printf("var.s is \"%d\"\n", ((int) ref->s));
  printf("var.i is \"%d\"\n", ((int) ref->i));

  /* get low 32 */
  pvar = (int*) ref;
  for (i=0; i < 32; i++) {
     printf("%d", ((*pvar & (1 << i)) ? 1 : 0) );
  }

  printf("-");

  /* get high 32 */
  pvar = ((int*) ref) + 1;
  for (i=0; i < 32; i++) {
     printf("%d", ((*pvar & (1 << i)) ? 1 : 0) );
  }

  printf("\n");

}









/*

Regular order on big endian system 

% ./sizes
var.z is "0"
var.s is "255"
var.i is "16777215"
11111111111111111111111100000000-11111111111111111111111111111111
var.z is "1"
var.s is "511"
var.i is "33554431"
11111111111111111111111110000000-11111111111111111111111111111111
var.z is "0"
var.s is "0"
var.i is "0"
00000000000000000000000000000000-11111111111111111111111111111111
var.z is "0"
var.s is "0"
var.i is "1"
10000000000000000000000000000000-11111111111111111111111111111111

*/

