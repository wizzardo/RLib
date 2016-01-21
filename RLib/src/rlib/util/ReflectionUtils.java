package rlib.util;

import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;

import java.lang.reflect.Field;

/**
 * Набор утильных методов по работе с рефлексией.
 * 
 * @author Ronn
 */
public final class ReflectionUtils {

	/**
	 * Получние всех полей указанного класса.
	 * 
	 * @param container контейнер полей.
	 * @param cs нужный нам класс.
	 * @param last до какого класса извлекаем.
	 * @param declared извлекать ли приватные поля.
	 * @param exceptions исключаемые поля.
	 */
	public static void addAllFields(final Array<Field> container, final Class<?> cs, final Class<?> last, final boolean declared, final String... exceptions) {

		Class<?> next = cs;

		while(next != null && next != last) {

			final Field[] fields = declared ? next.getDeclaredFields() : next.getFields();

			next = next.getSuperclass();

			if(fields.length < 1) {
				continue;
			}

			if(exceptions == null || exceptions.length < 1) {
				container.addAll(fields);
			} else {

				for(int i = 0, length = fields.length; i < length; i++) {

					final Field field = fields[i];

					if(ArrayUtils.contains(exceptions, field.getName())) {
						continue;
					}

					container.add(field);
				}
			}
		}
	}

	/**
	 * Получние всех полей указанного класса.
	 * 
	 * @param cs нужный нам класс.
	 * @param last до какого класса извлекаем.
	 * @param declared извлекать ли приватные поля.
	 * @param exceptions исключаемые поля.
	 */
	public static Array<Field> getAllFields(final Class<?> cs, final Class<?> last, final boolean declared, final String... exceptions) {
		final Array<Field> container = ArrayFactory.newArray(Field.class);
		addAllFields(container, cs, last, declared, exceptions);
		return container;
	}

	/**
	 * Получение значение поля объекта по названию поля.
	 * 
	 * @param object объект, чье поле хотим прочитать.
	 * @param fieldName название поле объекта.
	 * @return значение поля объекта.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Object object, String fieldName) {

		try {

			final Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);

			return (T) field.get(object);

		} catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Получение значение статического поля класса.
	 * 
	 * @param type интересуемый класс в котором хотим прочитать статическое
	 * поле.
	 * @param fieldName название статического поля.
	 * @return значение статического поля.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getStaticFieldValue(Class<?> type, String fieldName) {

		try {

			final Field field = type.getDeclaredField(fieldName);
			field.setAccessible(true);

			return (T) field.get(null);

		} catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Изменение значения статического поля класса.
	 * 
	 * @param type класс в котором меняем статическое поле.
	 * @param fieldName название статического поля.
	 * @param value новое значение статического поля.
	 */
	public static void setStaticFieldValue(Class<?> type, String fieldName, Object value) {

		try {
			final Field field = type.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(null, value);
		} catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private ReflectionUtils() {
		throw new IllegalArgumentException();
	}
}
