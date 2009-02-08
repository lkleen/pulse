package com.zutubi.pulse.transfer;

import org.hibernate.mapping.Table;
import org.hibernate.mapping.Column;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 *
 *
 */
public class MappingUtils
{
    public static String sqlSelectAll(org.hibernate.mapping.Table table)
    {
        return sqlSelectAll(new HibernateTable(table));
    }

    public static String sqlSelectAll(com.zutubi.pulse.transfer.Table table)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("select");
        String sep = " ";

        for (com.zutubi.pulse.transfer.Column column : table.getColumns())
        {
            builder.append(sep).append(column.getName());
            sep = ", ";
        }
        builder.append(" from ").append(table.getName());
        return builder.toString();
    }

    public static String sqlInsert(org.hibernate.mapping.Table table)
    {
        return sqlInsert(new HibernateTable(table));
    }

    public static String sqlInsert(com.zutubi.pulse.transfer.Table table)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into ").append(table.getName());
        String sep = " (";

        for (com.zutubi.pulse.transfer.Column column : table.getColumns())
        {
            builder.append(sep).append(column.getName());
            sep = ", ";
        }
        builder.append(") values (");

        sep = "";
        for (com.zutubi.pulse.transfer.Column column : table.getColumns())
        {
            builder.append(sep).append("?");
            sep = ", ";
        }
        builder.append(")");
        return builder.toString();
    }


    public static List<org.hibernate.mapping.Column> getColumns(org.hibernate.mapping.Table table)
    {
        List<org.hibernate.mapping.Column> columns = new LinkedList<org.hibernate.mapping.Column>();
        Iterator columnIterator = table.getColumnIterator();
        while (columnIterator.hasNext())
        {
            columns.add((org.hibernate.mapping.Column) columnIterator.next());
        }
        return columns;
    }

}