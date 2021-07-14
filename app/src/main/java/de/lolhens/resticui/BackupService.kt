package de.lolhens.resticui

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import de.lolhens.resticui.config.FolderConfig

class BackupService : JobService() {
    companion object {
        fun schedule(context: Context) {
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            //jobScheduler.cancel(0)
            println("LIST JOBS:")
            //jobScheduler.
            val contains = jobScheduler.allPendingJobs.any { job ->
                val name = job.service.className
                println(name)
                name == BackupService::class.java.name
            }
            println(contains)
            if (!contains) {
                val serviceComponent = ComponentName(context, BackupService::class.java)
                val builder = JobInfo.Builder(0, serviceComponent)
                //builder.setMinimumLatency(2 * 60 * 1000L)
                //builder.setOverrideDeadline(3 * 60 * 1000L)
                builder.setPersisted(true)
                builder.setPeriodic(15 * 60 * 1000L, 30 * 1000L)

                //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                //builder.setRequiresCharging(true)
                jobScheduler.schedule(builder.build())
            }
            jobScheduler.allPendingJobs.forEach { job ->
                println(job.service.className)
            }
        }

        fun startBackup(context: Context, callback: (() -> Unit)? = null) {
            val backup = Backup.instance(context)

            fun nextFolder(folders: List<FolderConfig>, callback: (() -> Unit)? = null) {
                if (folders.isEmpty()) {
                    if (callback != null) callback()
                } else {
                    val folder = folders.first()
                    fun next() = nextFolder(folders.drop(1), callback)

                    val started =
                        folder.schedule.lowercase() != "manual" && backup.backup(
                            context,
                            folder,
                            removeOld = true
                        ) {
                            next()
                        }
                    if (!started) {
                        next()
                    }
                }
            }

            nextFolder(backup.config.folders, callback)
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        startBackup(applicationContext) {
            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}