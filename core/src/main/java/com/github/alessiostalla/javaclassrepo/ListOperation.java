package com.github.alessiostalla.javaclassrepo;

import java.util.List;

/**
 * Created by alessio on 06/07/14.
 */
public interface ListOperation<T> {

    void execute(List<T> objects);

}
