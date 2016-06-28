package com.e16din.requestmanager;

import java.io.Serializable;

public class EmptyListResult extends ListResult implements Serializable {
    @Override
    public boolean isSuccess() {
        return true;
    }
}
