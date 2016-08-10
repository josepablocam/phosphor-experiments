# collect csv from python, calculate some simple stats and plot
#!/usr/bin/env Rscript
library(ggplot2)
library(plyr)

# arguments
args <- commandArgs(trailingOnly = T)
if (length(args) != 2) {
  stop("Usage: Rscript graph.R <results-file> <output-path>") 
}
DATA_PATH <- args[1]
OUTPUT_FOLDER <- args[2]

# load data
data <- read.csv(DATA_PATH)

# extract experiment characteristics from class name
split_names <- strsplit(as.character(data$class_name), "\\.")
loop_type <- sapply(split_names, function(x) x[[4]])
# provide clean names for loop type
loop_clean_name <- c("monotonic taint loop", "no-taint loop", "non-monotonic taint loop")
names(loop_clean_name) <- c("monotonic", "no", "nonmonotonic")
loop_type <- loop_clean_name[loop_type]
instr_type <- sapply(split_names, function(x) x[[5]])
data$loop_type <- loop_type
data$instr_type <- instr_type

# summarize the data into mean execution time and standard deviation of execution time acrosss
# all experiments
summarized <- ddply(data, 
                    .(loop_type, instr_type, test_name), 
                    summarize, 
                    mean_exec_time = mean(execution_time), stddev = sd(execution_time), ct = length(execution_time)
                    )

plotted <- 
  ggplot(summarized, aes(x = test_name, y = mean_exec_time)) + 
  geom_bar(aes(group = instr_type, fill = instr_type), stat = "identity", position="dodge") + 
  geom_errorbar(aes(ymax = mean_exec_time + stddev, ymin = mean_exec_time - stddev, group = instr_type), position = "dodge") +
  facet_wrap( ~ loop_type, scales = "free_x") +
  labs(
    x = "Start of Taint Relative to Loop", 
    y = "Mean Execution Time (ns)", 
    fill = "Instrumentation Type",
    title = paste(c("Loop Effect Comparison", " (Experiment Iterations ", summarized$ct[1], ")"), collapse = "")
    ) +
  theme(axis.text.x = element_text(angle = 45), legend.position = "top") 

free_scales <- plotted + facet_wrap(~loop_type, scales="free")

ggsave(filename = paste0(OUTPUT_FOLDER, "plot.pdf", collapse = "/"), plot = plotted)
ggsave(filename = paste0(OUTPUT_FOLDER, "/plot_free_scales.pdf", collapse = "/"), plot = free_scales)
