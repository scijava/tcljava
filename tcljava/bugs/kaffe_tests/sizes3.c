#include <stdio.h>

union combined {
  char z;
  int i;
};

typedef union combined combined;

void printjbits(combined * ref);

void main(int argc, char ** argv) {
  combined var;
  char * ptmp;
  char ctmp;

  memset(&var, 0xFF, 4); /* turn on all 32 bits */

  var.z = 0;
  printjbits(&var);

  memset(&var, 0xFF, 4); /* turn on all 32 bits */

  var.z = 1;
  printjbits(&var);

  memset(&var, 0xFF, 4); /* turn on all 32 bits */

  var.i = 0;
  printjbits(&var);

  memset(&var, 0xFF, 4); /* turn on all 32 bits */

  var.i = 1;
  printjbits(&var);


  memset(&var, 0xFF, 4); /* turn on all 64 bits */

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


void printjbits(combined * ref) {
  int* pvar;
  int i;

  printf("var.z is \"%d\"\n", ((int) ref->z));
  printf("var.i is \"%d\"\n", ((int) ref->i));

  pvar = (int*) ref;
  for (i=0; i < 32; i++) {
     printf("%d", ((*pvar & (1 << i)) ? 1 : 0) );
  }

  printf("\n");
}
