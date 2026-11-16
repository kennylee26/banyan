package cn.kennylee.banyan.commons.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 集合工具。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class CollUtils {

    private CollUtils() {
    }

    /**
     * 判断集合是否为空。
     *
     * @param collection 集合
     * @return 是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否非空。
     *
     * @param collection 集合
     * @return 是否非空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 按固定大小切分集合，用于分批处理。
     *
     * @param collection 原集合
     * @param size       每片大小；小于等于 0 时按 1 处理
     * @param <E>        元素类型
     * @return 分片列表；原集合为空时返回空列表
     */
    public static <E> List<List<E>> partition(Collection<E> collection, int size) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        int chunkSize = size <= 0 ? 1 : size;
        List<List<E>> chunks = new ArrayList<>();
        List<E> current = new ArrayList<>(chunkSize);
        for (E element : collection) {
            current.add(element);
            if (current.size() == chunkSize) {
                chunks.add(current);
                current = new ArrayList<>(chunkSize);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current);
        }
        return chunks;
    }
}
