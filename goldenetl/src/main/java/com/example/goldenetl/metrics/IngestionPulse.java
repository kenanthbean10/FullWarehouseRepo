package com.example.goldenetl.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
@Component
public class IngestionPulse {
private final AtomicLong recordsFetched = new AtomicLong(0);
    private final AtomicLong recordsTransformed = new AtomicLong(0);

private final AtomicLong recordsStored = new AtomicLong(0);
    private final AtomicLong recordsFailed = new AtomicLong(0);
//add extracted records
    public void AddFetched(int count)
{
    this.recordsFetched.addAndGet(count);
}
//stored records added successfully records
public void addStored()
{
    this.recordsStored.incrementAndGet();
  //  this.recordsTransformed.incrementAndGet(); //
}
//handles partial failures explicitly
    public void addFailed()
    {
        this.recordsFailed.incrementAndGet();
    }
    public long getRecordsFetched() {
        return recordsFetched.get();
    }

    public long getRecordsStored() {
        return recordsStored.get();
    }

    public long getRecordsTransformed() {
        return recordsTransformed.get();
    }

    public long getRecordsFailed() {
        return recordsFailed.get();
    }
    // Add this to your IngestionPulse class
    public void markAsTransformed() {
        this.recordsTransformed.incrementAndGet();
    }

}
