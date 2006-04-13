/********************************************************************************
 @COPYRIGHT@
 ********************************************************************************/
package com.zutubi.pulse.util;

import com.zutubi.pulse.core.util.TimeStamps;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.hsqldb.Types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TimeStampsType implements CompositeUserType
{
    private static final int[] TYPES = new int[]{Types.BIGINT, Types.BIGINT, Types.BIGINT};

    public int[] sqlTypes()
    {
        return TYPES;
    }

    public Class returnedClass()
    {
        return TimeStamps.class;
    }

    public boolean equals(Object x, Object y)
    {
        if (x == y)
        {
            return true;
        }
        if (x == null || y == null)
        {
            return false;
        }

        return x.equals(y);
    }

    public Object deepCopy(Object x)
    {
        if (x == null)
        {
            return null;
        }
        if (!(x instanceof TimeStamps))
        {
            return null;
        }
        TimeStamps result = new TimeStamps((TimeStamps) x);
        return result;
    }

    public boolean isMutable()
    {
        return true;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
            throws HibernateException, SQLException
    {

        Long queueTime = (Long) Hibernate.LONG.nullSafeGet(rs, names[0]);
        Long startTime = (Long) Hibernate.LONG.nullSafeGet(rs, names[1]);
        Long endTime = (Long) Hibernate.LONG.nullSafeGet(rs, names[2]);

        return new TimeStamps((queueTime != null) ? queueTime : -1, (startTime != null) ? startTime : -1, (endTime != null) ? endTime : -1);
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {

        if (value != null)
        {
            TimeStamps stamps = (TimeStamps) value;

            Hibernate.LONG.nullSafeSet(st, stamps.getQueueTime(), index);
            Hibernate.LONG.nullSafeSet(st, stamps.getStartTime(), index + 1);
            Hibernate.LONG.nullSafeSet(st, stamps.getEndTime(), index + 2);
        }
        else
        {
            Hibernate.LONG.nullSafeSet(st, null, index);
            Hibernate.LONG.nullSafeSet(st, null, index + 1);
            Hibernate.LONG.nullSafeSet(st, null, index + 2);
        }
    }

    public String[] getPropertyNames()
    {
        return new String[]{"queueTime", "startTime", "endTime"};
    }

    public Type[] getPropertyTypes()
    {
        return new Type[]{Hibernate.LONG, Hibernate.LONG, Hibernate.LONG};
    }

    public Object getPropertyValue(Object component, int property)
    {
        TimeStamps stamps = (TimeStamps) component;
        if (property == 0)
        {
            return stamps.getQueueTime();
        }
        else if (property == 1)
        {
            return stamps.getStartTime();
        }
        else
        {
            return stamps.getEndTime();
        }
    }

    public void setPropertyValue(
            Object component,
            int property,
            Object value)
    {
        TimeStamps stamps = (TimeStamps) component;
        if (property == 0)
        {
            stamps.setQueueTime((Long) value);
        }
        else if (property == 1)
        {
            stamps.setStartTime((Long) value);
        }
        else
        {
            stamps.setEndTime((Long) value);
        }
    }

    public Object assemble(
            Serializable cached,
            SessionImplementor session,
            Object owner)
    {

        return deepCopy(cached);
    }

    public Serializable disassemble(Object value, SessionImplementor session)
    {
        return (Serializable) deepCopy(value);
    }

    public int hashCode(Object x) throws HibernateException
    {
        return x.hashCode();
    }

    public Object replace(Object original, Object target, SessionImplementor session, Object owner) throws HibernateException
    {
        return new TimeStamps((TimeStamps) original);
    }
}
