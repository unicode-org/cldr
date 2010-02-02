package org.unicode.cldr.web;

public class IntHash<T> {
    public final static int HASH_SIZE = 2048;
    public final static int MAX_SIZE = XPathTable.MAX_SIZE;
    public final static int BUCKET_COUNT=MAX_SIZE/HASH_SIZE;
    private Object[][] hashedIds = new Object[BUCKET_COUNT][]; 
    public void clear() {
        for(int i=0;i<hashedIds.length;i++) {
            hashedIds[i]=null;
        }
//        hashedIds = new Object[BUCKET_COUNT][];
    }
    private final int idToBucket(int id) {
        return id/HASH_SIZE;
    }
    public String stats() {
        int filled=0;
        int lastbuck=0;
        for(int i=0;i<BUCKET_COUNT;i++) {
            if(hashedIds[i]!=null) {
                filled++;
                lastbuck=i;
            }
        }        
        return "IntHash<"+"T" + "> Max:"+MAX_SIZE+", HASH:"+HASH_SIZE+", NRBUCKETS:"+filled+"/"+BUCKET_COUNT+" : last bucket="+lastbuck+", greatest max="+(lastbuck*HASH_SIZE);
    }
    @SuppressWarnings("unchecked")
    public final T put(int id, T str) {
        try {
            int buckid=idToBucket(id);
            T[] bucket = (T[])hashedIds[buckid];
            if(bucket==null) {
                bucket = (T[])new Object[HASH_SIZE];
                hashedIds[buckid] = bucket;
            }
            return bucket[id%HASH_SIZE] = str;
        } catch(ArrayIndexOutOfBoundsException aioob) {
            if(id>MAX_SIZE) throw new InternalError("Exceeded max " + MAX_SIZE + " @ " + id);
	    System.err.println("aioob: id"+id+", buckid"+idToBucket(id)+", hashedIdsLen"+hashedIds.length);
            throw aioob;
        }
    }
    public final T get(int id) {
        try {
            @SuppressWarnings("unchecked")
            T[] bucket = (T[])hashedIds[idToBucket(id)];
            if(bucket==null) return null; // no bucket = no id.
            return bucket[id%HASH_SIZE];
        } catch(ArrayIndexOutOfBoundsException aioob) {
            if(id>MAX_SIZE) throw new InternalError("Exceeded max " + MAX_SIZE + " @ " + id);
            throw aioob;
        }
    }
}
