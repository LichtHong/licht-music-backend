package work.licht.music.gson.serializer;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.Setter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Gson2JsonRedisSerializer<T> implements RedisSerializer<T> {

	@Setter
    private Gson gson;
	private final Type type;
	private static final byte[] EMPTY_ARRAY = new byte[0];
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public Gson2JsonRedisSerializer(Class<T> clazz) {
		this(new Gson(), clazz);
	}

	public Gson2JsonRedisSerializer(Type type) {
		this(new Gson(), type);
	}

	public Gson2JsonRedisSerializer(Gson gson, Class<T> clazz) {
		Assert.notNull(gson, "Gson must not be null");
		Assert.notNull(clazz, "Java class must not be null");
		this.type = getJavaType(clazz);
		this.gson = gson;
	}

	public Gson2JsonRedisSerializer(Gson gson, Type type) {
		Assert.notNull(gson, "ObjectMapper must not be null!");
		Assert.notNull(type, "Java type must not be null!");
		this.gson = gson;
		this.type = type;
	}

	@Override
	public T deserialize(@Nullable byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		try {
			String json = new String(bytes, DEFAULT_CHARSET);
			return this.gson.fromJson(json, type);
		} catch (Exception ex) {
			throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	public byte[] serialize(@Nullable Object t) throws SerializationException {
		if (t == null) {
			return EMPTY_ARRAY;
		}
		try {
			return this.gson.toJson(t).getBytes(DEFAULT_CHARSET);
		} catch (Exception ex) {
			throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

    protected Type getJavaType(Class<?> clazz) {
		return new TypeToken<T>(clazz) {}.getType();
	}
}
