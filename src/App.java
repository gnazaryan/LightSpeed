import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class App {

    /**
     * Creates a deep copy of the given object.
     * 
     * This method recursively copies the object and all its fields for the below types
     * Primitives, Immutables, Arrays, Collections, Maps, Objects, Null Values
     *
     * @param object The input object to make a deep copy.
     * @return The deep copied resulting object
     */
    public static <T> T deepCopy(T object) throws IllegalAccessException, InstantiationException {
        //Use IdentityHashMap to cache the object references that have already been 
        //deep copied, specifically use the IdentityHashMap to make sure the references are checked as keys
        Map<Object, Object> copiedObjects = new IdentityHashMap<>();
        return deepCopyInternal(object, copiedObjects);
    }

    /**
     * The main deep copy internal method, which runs recursively
     *
     * @param object The input object to make a deep copy.
     * @param copiedObjects Map to cache already copied objects.
     * @return The deep copied resulting object
     */
    private static <T> T deepCopyInternal(T object, Map<Object, Object> copiedObjects) throws IllegalAccessException, InstantiationException {
        // Handle the null case
        if (object == null) {
            return null;
        }

        // Make sure the object is not in the cache, in case it is, skip and return
        if (copiedObjects.containsKey(object)) {
            return (T) copiedObjects.get(object);
        }

        // Handle the objects that not need to be copied (object primitives and String)
        if (object instanceof Integer ||
            object instanceof Long ||
            object instanceof Boolean ||
            object instanceof Short ||
            object instanceof Float ||
            object instanceof Double ||
            object instanceof String) {
            return object;
        }

        // Create the new empty instance of the object to be deep copied
        T copy = createInstance(object);
        // Cache the copy
        copiedObjects.put(object, copy);

        // Handle the Arrays and make sure all elements are deep copied
        if (object.getClass().isArray()) {
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(object, i);
                // Recursively deep copy each element
                Object copyElement = deepCopyInternal(element, copiedObjects);
                Array.set(copy, i, copyElement);
            }
            return copy;
        }

        // Handle the collections, iterate and deep copy each item
        if (object instanceof Collection) {
            Collection<Object> originalCollection = (Collection<Object>) object;
            Collection<Object> copyCollection = (Collection<Object>) copy;
            for (Object item : originalCollection) {
                // Recursively call for all collection items
                Object copyElement = deepCopyInternal(item, copiedObjects);
                copyCollection.add(copyElement);
            }
            return copy;
        }

        // Handle the Maps, iterate and deep copy both the keys and values
        if (object instanceof Map) {
            Map<Object, Object> originalMap = (Map<Object, Object>) object;
            Map<Object, Object> copyMap = (Map<Object, Object>) copy;
            for (Map.Entry<Object, Object> entry : originalMap.entrySet()) {
                // Recursively call and deep copy the key
                Object copyKey = deepCopyInternal(entry.getKey(), copiedObjects);
                //Recursively call and deep copy the value
                Object copyValue = deepCopyInternal(entry.getValue(), copiedObjects);
                copyMap.put(copyKey, copyValue);
            }
            return copy;
        }

        // Handle the objects, iterate over all fields and make a deep copy of values
        // Also hamndle the inheritence, access all superclasses and deep copy the field values
        Class<?> objClass = object.getClass();
        // While the superclass is not null iterate over hierarchy
        while (objClass != null) {
            Field[] fields = objClass.getDeclaredFields();
            for (Field field : fields) {
                // Make sure the field is accessable
                field.setAccessible(true);
                Object fieldValue = field.get(object);
                Object copyFieldValue = deepCopyInternal(fieldValue, copiedObjects); // Recursive call for field values
                field.set(copy, copyFieldValue);
            }
            // Make sure to not miss the superclass
            objClass = objClass.getSuperclass();
        }
        return copy;
    }

    private static <T> T createInstance(T obj) throws InstantiationException {
        Class<?> clazz = obj.getClass();
        try {
            // First try to initiate the instance with default constructor
            return (T) clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // If the default constructor doesn't work try to make the instance using the unsafe
            return (T) getUnsafe().allocateInstance(clazz);
        } catch (Exception e) {
            // Catch the rest of the exceptions
            throw new RuntimeException("Can't create instance of " + clazz.getName() + ": " + e.getMessage());
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access Unsafe", e);
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> favoriteBooks = new ArrayList<>();
        favoriteBooks.add("Closer Together");
        favoriteBooks.add("Love and Math");
        favoriteBooks.add("Coraline");
        favoriteBooks.add("Fortunately The Milk");
        
        Man me = new Man("Grigor Nazaryan", 37, favoriteBooks);
        Man meToo = deepCopy(me);
        System.out.println(meToo.getName());
        System.out.println(meToo.getAge());
        for (String book: meToo.getFavoriteBooks()) {
            System.out.println(book);
        }
    }
}
