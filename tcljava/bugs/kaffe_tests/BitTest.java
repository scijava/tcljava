public class BitTest {
    public static void main(String[] argv) {
	/*
	int num = -8;
	
	System.out.println("num  is " + num);
	System.out.println("num1 is " + (num >>> 8));
	System.out.println("num2 is " + (num >> 8));
	*/


	short s = -0x7FFF;
	//short s = 0x7F01;

	int b1 = (byte) s;
	int b2 = (byte) (s & 0xFF);


	System.out.println("b1  is " + b1);
	System.out.println("b2  is " + b2);




	long l = -0x7FFFFFFFFFFFFFFF;

	int i1 = (int) l;
	int i2 = (int) (l & 0xFFFFFFFF);

	System.out.println("i1  is " + i1);
	System.out.println("i2  is " + i2);

	




	/*

	System.out.println("s1  is " + ((byte) s));
	System.out.println("s2  is " + ((byte) (s & 0x00FF)));

	*/

	//System.out.println("s3  is " + ((byte)((s >> 8) & 0xFF)));
	//System.out.println("s4  is " + ((byte)((s >>> 8) & 0xFF)));
	

	/*
	int i = 0xFFFF0000;

	System.out.println("i  is " + i);

	System.out.println("i1 is " + (i >>> 16));
	System.out.println("i2 is " + (i >> 16));
	*/
    }

} 



// 0x0010  =  1 * pow(2,9) = 512 
