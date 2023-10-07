# VectorQuantization

• Vector quantization (VQ) is a block-coding technique that quantizes blocks of data instead of single sample. VQ exploits relation existing between neighboring signal samples by quantizing them together.

• In general, a VQ scheme can be divided into two parts: the encoding procedure, and the decoding procedure which is depicted in figure.

• At, the encoder, input image is partitioned into a set of non- overlapping image blocks. The closest code word in the code hook is then found for each image block.
• Here, the closest code word for a given block is the one in the code book that has the minimum squared Euclidean distance from the input block.
• Next, the corresponding index for each searched closest code word is transmitted to the decoder.
• Compression is achieved because the Indices of the closest code words in the code book are sent to the decoder instead of the image blocks themselves.
• The goal of VQ code-book generation is to find an optimal code book that yields the lowest possible distortion when compared with all other code books of the same size.
• VQ performance is directly proportional to the code-book size and the vector size.
• The computational complexity in a VQ technique increases exponentially with the size of the vector blocks.
• Therefore, the blocks used by VQ are usually small. The encoder searches the code book and attempts to minimize the distortion between the original image block and the chosen vector from the code book according to some distortion metric.
• The search complexity increases with the number of vectors in the code book. To minimize the search complexity, the tree search vector quantization scheme was introduced.
• VQ can be used to compress an image both in the spatial domain and in the frequency domain.
• Vector quantization is a lossy data-compression scheme based on the principles of block coding.
• A vector quantizer maps a data set in an n-dimensional data space into a finite sect of vectors. Each vector is called a code vector or a code word.
• The set of all code words is called a code book. Each input vector can be associated with an index of a code word and this index is transferred instead of the original vector.
• The index can be decoded to get the code word that it represented.
