import numpy as np
import matplotlib.pyplot as plt

FILE_SIZE = 9782865*8

times = [41.125266313552856,72.58611536026001,104.52553391456604,135.52276420593262,176.56680178642273,238.09621906280518]
times = np.array(times)
throughputs = FILE_SIZE / times
probs= [0,0.1,0.2,0.3,0.4,0.5]

plt.plot(probs, throughputs, '-r')
plt.title('Loss Probability vs. Throughput (bps)')
plt.xlabel('Loss Probability', color='#1C2833')
plt.ylabel('Throughput (bps)', color='#1C2833')
plt.legend(loc='upper left')
plt.grid()
plt.show()

times = [72.58611536026001, 45.220324754714966, 31.541899919509888, 25.921935081481934, 22.008392810821533]
times = np.array(times)
throughputs = FILE_SIZE / times
N= [20,40,60,80,100]

plt.plot(N, throughputs, '-r')
plt.title('Window Size (N) vs. Throughput (bps)')
plt.xlabel('Window Size (N)', color='#1C2833')
plt.ylabel('Throughput (bps)', color='#1C2833')
plt.legend(loc='upper left')
plt.grid()
plt.show()