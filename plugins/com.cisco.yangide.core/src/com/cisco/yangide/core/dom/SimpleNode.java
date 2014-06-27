/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.cisco.yangide.core.dom;

/**
 * @author Konstantin Zaitsev
 * @date Jun 26, 2014
 */
public class SimpleNode<T> extends ASTNode {
    /** Name. */
    private String nodeName;

    private T value;

    public SimpleNode(ASTNode parent, String nodeName, T value) {
        super(parent);
        this.nodeName = nodeName;
        this.value = value;
    }

    /**
     * @return the value
     */
    public T getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }
}
