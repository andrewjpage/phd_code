
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/** From http://www.churchillobjects.com/c/13029.html
* This is not efficient and should be removed later with a better method
*/
public class ObjectMeter {

		/**
		 * Determines the size of an object in bytes when it is serialized.
		 * This should not be used for anything other than optimization
		 * testing since it can be memory and processor intensive.
		 */
		public static int getObjectSize(Object object) {
			if(object==null) {
					System.err.println("Object Meter - Object is null. Cannot measure object size.");
					return -1;
				}
			try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(object);
					byte[] bytes = baos.toByteArray();
					oos.close();
					baos.close();
					return bytes.length;
				} catch(Exception e) {
					System.err.println("Object Meter - Cannot measure object size.");
				}
			return -1;
		}
	}

